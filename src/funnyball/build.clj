(ns funnyball.build
  (:use (incanter io core)))

;; Parses any numeric value out of a string- used to get numeric seed values out of seeds in dataset like "W1"
(defn parse-int [x]
  (Integer/parseInt (apply str (filter #(Character/isDigit %) x))))

(def not-nil? (complement nil?))

(defn calc-mean [sq]
  (def non-nil-sq (filter not-nil? sq))
    (if (empty? non-nil-sq)
        0
        (/ (reduce + non-nil-sq) (count non-nil-sq))))

; (def teams-dataset (read-dataset "./kaggle_data/teams.csv" :header true))

(defn regular-season-dataset []
  (read-dataset "./kaggle_data/regular_season_results.csv" :header true))

(def season-win-loss-records
  ($group-by [:wteam :lteam :season] (regular-season-dataset)))

;;returns only the rows of the sagp rating where rating_day_num is the max(rating_day_num) for that season

(defn tourney-results-dataset []
  (sel
    (read-dataset "./kaggle_data/tourney_results.csv" :header true)
    :cols
    [:season :wteam :lteam]))

(defn tourney-seeds-dataset []
  (read-dataset "./kaggle_data/tourney_seeds.csv" :header true))

(def current-tourney-seeds-dataset
  (read-dataset "./kaggle_data_new/tourney_seeds.csv" :header true))

(def teams-in-current-tourney
  (sort ($ :team current-tourney-seeds-dataset)))

(defn current-tourney-cartesian-product-dataset[]
  (col-names (to-dataset (for [x teams-in-current-tourney
                                y (filter #(> %1 x) teams-in-current-tourney)]
                              [x y]))
             [:team1 :team2]))

(def cleaned-tourney-seeds-dataset
  (transform-col (tourney-seeds-dataset) :seed
    #(parse-int %1)))

;Ratings of each team right before the tournament started
(def sagp-ratings-dataset
  ($where
    {:rating_day_num {:$eq 133}}
    (read-dataset "./kaggle_data/sagp_weekly_ratings.csv" :header true)))

;; Never mind it's always 133
; (defn sagp-max-daynum-for-season []
;   ($rollup :max :rating_day_num :season (sagp-ratings-dataset)))

(defn lookup-sagp [team season]
  (def temp ($ :rating
       ($where {:team team :season season}
                      sagp-ratings-dataset)))
  (if (instance? Double temp)
    temp
    nil))

;; returns the top seeded n teams in a given season.  n is an optional parameter that will default to 4.  n should be divisible by 4, or else the results may be non-deterministic
(defn top-n-seeded-teams-in-season [season & [n]]
  (head
    (or n 4)
    ($order
      :seed
      :asc
      ($where
        {:season season}
        cleaned-tourney-seeds-dataset))))

(defn calc-win-loss [team1 team2 season]
  ; (println "calculating win/loss ratio of " team1 " and " team2 " in season " season)
  (def wins
    (or
      (nrow
        (get
          season-win-loss-records
          {:season season :lteam team2 :wteam team1}))
      0))
  ; (println wins "wins")
  (def losses
    (or
      (nrow
        (get
          season-win-loss-records
          {:season season :lteam team1 :wteam team2}))
      0))
  ; (println losses "losses")
  (def total (+ wins losses))
  (if (= team1 team2)
    nil
    (if (= 0 total)
      nil
      (/
        wins
        total))))

(defn win-loss-vs-top-n-seeds [team season & [n]]
  (calc-mean
    (map
      #(calc-win-loss team %1 season)
      ($
        :team
        (top-n-seeded-teams-in-season season n)))))

(defn cleaned-tourney-wseeds-dataset []
  (col-names cleaned-tourney-seeds-dataset [:season :wseed :wteam]))

(defn cleaned-tourney-lseeds-dataset []
  (col-names cleaned-tourney-seeds-dataset [:season :lseed :lteam]))

(defn add-win-seed [dataset]
  ($join [[:season :wteam][:season :wteam]]
    (cleaned-tourney-wseeds-dataset)
    dataset))

(defn add-win-and-loss-seeds [dataset]
  ($join [[:season :lteam][:season :lteam]]
    (cleaned-tourney-lseeds-dataset)
    (add-win-seed dataset)))

;;add a column seed advantage indicating how much the winning team was favored in seed rank
(defn add-seed-advantage [dataset]
  (add-derived-column :seed-advantage [:wseed :lseed]
    (fn [wseed lseed] (- lseed wseed))
    (add-win-and-loss-seeds
      dataset)))

;;add a column win-loss indicating the win-loss ratio of the winning team over the losing team in the regular season.  Unfortunately i found that regular season matchups were very rare, and not predictive.
(defn add-regular-season-win-loss [dataset]
  (add-derived-column :reg-win-loss [:wteam :lteam :season]
    (fn [wteam lteam season] (calc-win-loss wteam lteam season))
    dataset))

;;add a column win-loss indicating the win-loss ratio of the winning team over the tournament seeded teams
(defn add-regular-season-seeded-team-win-loss-advantage [dataset]
  (add-derived-column :seed-win-loss-advantage-64 [:wteam-seed-win-loss-64 :lteam-seed-win-loss-64]
    (fn [wteamwl lteamwl] (- wteamwl lteamwl))
    (add-derived-column :lteam-seed-win-loss-64 [:lteam :season]
      (fn [lteam season] (win-loss-vs-top-n-seeds lteam season 64))
      (add-derived-column :wteam-seed-win-loss-64 [:wteam :season]
        (fn [wteam season] (win-loss-vs-top-n-seeds wteam season 64))
        dataset))))

;;add a column sapg-diff indicating the sagp rating difference between the winning and losing team
(defn add-sagp-advantage [dataset]
  (add-derived-column :sagp-advantage [:wteam-sagp :lteam-sagp]
    (fn [wteamsagp lteamsagp] (if (or (nil? wteamsagp)
                                      (nil? lteamsagp))
                                  nil
                                  (- wteamsagp lteamsagp)))
    (add-derived-column :lteam-sagp [:lteam :season]
      (fn [lteam season] (lookup-sagp lteam season))
      (add-derived-column :wteam-sagp [:wteam :season]
        (fn [wteam season] (lookup-sagp wteam season))
        dataset))))

;;ADD ANY NEW MODEL FEATURES HERE

;; reduce dataset for winners to columns that can be inverted, and add a did_win column set to true
(defn reduce-dataset[dataset]
  (add-derived-column
    :did-win
    []
    (fn [] (= 1 1))
    (col-names
      (sel
        dataset
        :cols
        [:season :wteam :lteam :seed-advantage :seed-win-loss-advantage-64 :reg-win-loss :sagp-advantage])
      [:season :team1 :team2 :seed-advantage :seed-win-loss-advantage-64 :reg-win-loss :sagp-advantage])))

(defn complete-dataset[]
  (reduce-dataset
    (add-sagp-advantage
      (add-regular-season-win-loss
        (add-regular-season-seeded-team-win-loss-advantage
          (add-seed-advantage
            (tourney-results-dataset)))))))

;; Takes a dataset of winning teams and returns the inverted stats for the losing teams and sets the did_win to 0
(defn invert-dataset[dataset]
  (sel
    (transform-col
      (transform-col
        (transform-col
          (transform-col
            (transform-col
              dataset
              :seed-advantage
              #(- 0 %1))
            :seed-win-loss-advantage-64
            #(- 0 %1))
          :did-win
          #(not %1))
        :reg-win-loss
        (fn [regwl]
          (if (nil? regwl)
            nil
            (if (= 0 regwl)
              1
              (/ 1 regwl)))))
      :sagp-advantage
      (fn [sagpadv]
        (if (nil? sagpadv)
          nil
          (- 0 sagpadv))))
    :cols
    [:season :team2 :team1 :seed-advantage :seed-win-loss-advantage-64 :reg-win-loss :sagp-advantage :did-win]))

;; unions a dataset for winning teams with an equivalent dataset for losing teams
(defn add-inverse-dataset[dataset]
  (conj-rows
    dataset
    (invert-dataset dataset)))

;;add a column obs_id
(defn add-obs-id [dataset]
  (add-derived-column :obs-id [:team1 :team2 :season]
    (fn [team1 team2 season] (str season "_" team1 "_" team2))
    dataset))

(defn prediction-dataset[]
  (add-obs-id
    (add-inverse-dataset
      (complete-dataset))))

;;reduce dataset to the columns that we need for the prediction model
(defn output-dataset []
  (sel
    (prediction-dataset)
    :cols
    [:obs-id :did-win :seed-advantage :seed-win-loss-advantage-64]))

;;reduce dataset to the columns that we need for the prediction model in r and convert to r formats
(defn output-r-dataset []
  (transform-col
    (sel
      (prediction-dataset)
      :cols
      [:did-win :seed-advantage :seed-win-loss-advantage-64 :sagp-advantage])
     :seed-win-loss-advantage-64
     #(float %1)))

(defn current-complete-dataset[]
  (reduce-dataset
    (add-sagp-advantage
      (add-regular-season-win-loss
        (add-regular-season-seeded-team-win-loss-advantage
          (add-seed-advantage
            (tourney-results-dataset)))))))

(defn current-dataset[]
  (add-obs-id
    (current-complete-dataset)))

;;reduce dataset to the columns that we need for the prediction model in r and convert to r formats
(defn current-r-dataset []
  (transform-col
    (sel
      (current-dataset)
      :cols
      [:obs-id :seed-advantage :seed-win-loss-advantage-64 :sagp-advantage])
     :seed-win-loss-advantage-64
     #(float %1)))

(defn save-to-file [& [dataset]]
  (save
    (or dataset (output-dataset))
    "./output/input.csv"))

(defn save-to-r-file [& [dataset]]
  (save
    (or dataset (output-r-dataset))
    "./output/input-r.csv"))

(defn save-current-season-to-r-file [& [dataset]]
  (save
    (or dataset (current-r-dataset))
    "./output/current-r.csv"))
