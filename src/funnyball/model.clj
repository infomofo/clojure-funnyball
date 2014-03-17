(ns funnyball.model
  (:use 
  	(incanter io core)
  )
)

(defn input-dataset []
	(read-dataset "./output/input.csv" :header true)
)