(ns funnyball.model
  (:use
  	(incanter io core)
  )
)

(defn input-dataset []
	(read-dataset "./output/input-r.csv" :header true)
)
