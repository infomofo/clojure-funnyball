(ns funnyball.predict (:use (incanter io
                                      core
                                      charts)))



(defn input-dataset []
	(read-dataset "./output/submission.csv"
                :header true))

(defn format-kaggle-output[]
  (col-names (sel (input-dataset)
                  :cols
                  [:obs.id :probability.true])
             [:id :pred]))

(defn output-kaggle-predictions []
  (save (format-kaggle-output)
        "./output/kaggle_submission.csv"))
