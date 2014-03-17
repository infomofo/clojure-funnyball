(ns funnyball.model (:use (incanter io
                                    core
                                    charts)))

(defn input-dataset []
	(read-dataset "./output/input-r.csv"
                :header true)
)

(defn view-dataset[]
  (with-data (input-dataset)
    (view (set-alpha (scatter-plot :seed-advantage
                                   :seed-win-loss-advantage-64
                                   :group-by :did-win
                                   :legend true)
                     0.2))))
