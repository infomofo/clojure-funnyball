(ns funnyball.build
  (:use (incanter io core))
  )

;; Parses any numeric value out of a string- used to get numeric seed values out of seeds in dataset like "W1"
(defn parse-int [x] 
	(Integer/parseInt (apply str (filter #(Character/isDigit %) x)))
)

(def not-nil? (complement nil?))

(defn calc-mean [sq]
	(def non-nil-sq (filter not-nil? sq))
  	(if (empty? non-nil-sq)
      	0
      	(/ (reduce + non-nil-sq) (count non-nil-sq))
    )
)

; (def teams-dataset (read-dataset "./kaggle_data/teams.csv" :header true))

(defn regular-season-dataset []
	(read-dataset "./kaggle_data/regular_season_results.csv" :header true)
)

(def season-win-loss-records
	($group-by [:wteam :lteam :season] (regular-season-dataset))
)

(defn tourney-results-dataset []
	(sel
		(read-dataset "./kaggle_data/tourney_results.csv" :header true)
		:cols
		[:season :wteam :lteam]
	)
)

(defn tourney-seeds-dataset []
	(read-dataset "./kaggle_data/tourney_seeds.csv" :header true)
)

(def cleaned-tourney-seeds-dataset 
	(transform-col (tourney-seeds-dataset) :seed 
		#(parse-int %1)
	)
)

;;Ratings of each team right before the tournament started
; (defn sagp-ratings-dataset []
; 	($rollup
; 		:max 
; 		($where
; 			{:rating_day_num {:$lt 136}}
; 			(read-dataset "./kaggle_data/sagp_weekly_ratings.csv" :header true)
; 		)
; 	)
; )

;; returns the top seeded n teams in a given season.  n is an optional parameter that will default to 4.  n should be divisible by 4, or else the results may be non-deterministic
(defn top-n-seeded-teams-in-season [season & [n]] 
	(head
		(or n 4)
		($order
			:seed
			:asc
			($where 
				{:season season} 
				cleaned-tourney-seeds-dataset
			)
		)
	)
)

(defn calc-win-loss [team1 team2 season]
	; (println "calculating win/loss ratio of " team1 " and " team2 " in season " season)
	(def wins 
		(or
			(nrow 
				(get 
					season-win-loss-records
					{:season season :lteam team2 :wteam team1}
				)
			)
			0
		)
	)
	; (println wins "wins")
	(def losses 
		(or
			(nrow 
				(get 
					season-win-loss-records
					{:season season :lteam team1 :wteam team2}
				)
			)
			0
		)
	)
	; (println losses "losses")
	(def total (+ wins losses))
	(if (= team1 team2)
		nil
		(if (= 0 total)
			nil
			(/ 
				wins 
				total
			)
		)
	)
)

(defn win-loss-vs-top-n-seeds [team season & [n]]
	(calc-mean
		(map
			#(calc-win-loss team %1 season)
			($
				:team
				(top-n-seeded-teams-in-season season n)
			)
		)
	)
)

(defn cleaned-tourney-wseeds-dataset [] 
	(col-names cleaned-tourney-seeds-dataset [:season :wseed :wteam])
)

(defn cleaned-tourney-lseeds-dataset [] 
	(col-names cleaned-tourney-seeds-dataset [:season :lseed :lteam])
)

(defn tourney-with-win-seeds [] 
	($join [[:season :wteam][:season :wteam]]
		(cleaned-tourney-wseeds-dataset)
		(tourney-results-dataset)
	)
)

(defn tourney-with-win-and-loss-seeds [] 
	($join [[:season :lteam][:season :lteam]]
		(cleaned-tourney-lseeds-dataset)
		(tourney-with-win-seeds)
	)
)
;;add a column seed advantage indicating how much the winning team was favored in seed rank
(defn tourney-with-seed-advantage [] 
	(add-derived-column :seed-advantage [:wseed :lseed] 
		(fn [wseed lseed] (- lseed wseed)) 
		(tourney-with-win-and-loss-seeds)
	)
)
;;add a column win-loss indicating the win-loss ratio of the winning team over the losing team in the regular season.  Unfortunately i found that regular season matchups were very rare, and not predictive.
(defn tourney-with-regular-season-win-loss [] 
	(add-derived-column :reg-win-loss [:wteam :lteam :season] 
		(fn [wteam lteam season] (calc-win-loss wteam lteam season)) 
		(tourney-with-seed-advantage)
	)
)

;;add a column win-loss indicating the win-loss ratio of the winning team over the tournament seeded teams
(defn tourney-with-regular-season-seeded-team-win-loss-advantage []
	(add-derived-column :seed-win-loss-advantage-64 [:wteam-seed-win-loss-64 :lteam-seed-win-loss-64]
		(fn [wteamwl lteamwl] (- wteamwl lteamwl))
		(add-derived-column :lteam-seed-win-loss-64 [:lteam :season] 
			(fn [lteam season] (win-loss-vs-top-n-seeds lteam season 64)) 
			(add-derived-column :wteam-seed-win-loss-64 [:wteam :season] 
				(fn [wteam season] (win-loss-vs-top-n-seeds wteam season 64)) 
				(tourney-with-regular-season-win-loss)
			)
		)
	)
)

;;ADD ANY NEW MODEL FEATURES HERE

;;add a column obs_id
(defn tourney-with-obs_id []
	(add-derived-column :obs_id [:wteam :lteam :season]
		(fn [wteam lteam season] (str season "_" wteam "_" lteam))
		(tourney-with-regular-season-seeded-team-win-loss-advantage)
	)
)

;;reduce dataset to the columns that we need for the prediction model
(defn prediction-dataset []
	(sel
		(tourney-with-obs_id)
		:cols
		[:obs_id :seed-advantage :seed-win-loss-advantage-64]
	)
)

(defn save-to-file [& [dataset]]
  (save 
  	(or dataset (prediction-dataset)) 
  	"./output/input.csv"
  )
)