(ns funnyball.build
  (:use (incanter io core))
  )

;; Parses any numeric value out of a string- used to get numeric seed values out of seeds in dataset like "W1"
(defn parse-int [x] (Integer/parseInt (apply str (filter #(Character/isDigit %) x))))
; (def teams-dataset (read-dataset "./kaggle_data/teams.csv" :header true))
; (def regular-season-dataset 
	; (read-dataset "./kaggle_data/regular_season_results.csv" :header true))
(def tourney-results-dataset 
	(read-dataset "./kaggle_data/tourney_results.csv" :header true))
(def tourney-seeds-dataset 
	(read-dataset "./kaggle_data/tourney_seeds.csv" :header true))
(def cleaned-tourney-seeds-dataset (transform-col tourney-seeds-dataset :seed (fn [seed] (parse-int seed)) ))
(def cleaned-tourney-wseeds-dataset (col-names cleaned-tourney-seeds-dataset [:season :wseed :wteam]))
(def cleaned-tourney-lseeds-dataset (col-names cleaned-tourney-seeds-dataset [:season :lseed :lteam]))
(def tourney-with-win-seeds ($join [[:season :wteam][:season :wteam]]
	cleaned-tourney-wseeds-dataset
	tourney-results-dataset
))
(def tourney-with-win-and-loss-seeds ($join [[:season :lteam][:season :lteam]]
	cleaned-tourney-lseeds-dataset
	tourney-with-win-seeds
))
;;add a column seed advantage indicating how much the winning team was favored in seed rank
(def tourney-with-seed-advantage (add-derived-column :seed-advantage [:wseed :lseed] (fn [wseed lseed] (- lseed wseed)) tourney-with-win-and-loss-seeds))

(defn save-to-file [dataset]
  (save dataset "./output/input.csv"))
