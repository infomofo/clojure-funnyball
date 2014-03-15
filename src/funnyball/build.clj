(ns funnyball.build
  (:use (incanter io core))
  )

;; Parses any numeric value out of a string- used to get numeric seed values out of seeds in dataset like "W1"
(defn parse-int [x] 
	(Integer/parseInt (apply str (filter #(Character/isDigit %) x)))
)
; (def teams-dataset (read-dataset "./kaggle_data/teams.csv" :header true))

(defn regular-season-dataset []
	(read-dataset "./kaggle_data/regular_season_results.csv" :header true)
)

(defn season-win-loss-records []
	($group-by [:wteam :lteam :season] (regular-season-dataset))
)

(defn calc-win-loss [team1 team2 season]
	(def wins 
		(or
			(nrow 
				(get 
					(season-win-loss-records)
					{:season season :lteam team2 :wteam team1}
				)
			)
			0
		)
	)
	(def losses 
		(or
			(nrow 
				(get 
					(season-win-loss-records)
					{:season season :lteam team1 :wteam team2}
				)
			)
			0
		)
	)
	(if (= losses 0)
		nil
		(/ wins (+ wins losses))
	)
)

(defn tourney-results-dataset []
	(read-dataset "./kaggle_data/tourney_results.csv" :header true)
)

(defn tourney-seeds-dataset []
	(read-dataset "./kaggle_data/tourney_seeds.csv" :header true)
)

(def cleaned-tourney-seeds-dataset 
	(transform-col (tourney-seeds-dataset) :seed 
		(fn [seed] (parse-int seed))
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


(defn save-to-file [dataset]
  (save dataset "./output/input.csv"))
