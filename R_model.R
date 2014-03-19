library("ggplot2")
library("caret")
library("randomForest")
library("glmnet")
library("ROCR")

data <- read.table("output/input-r.csv_bak", header=TRUE, sep=",")

# Plot
ggplot(data, aes(seed.advantage, sagp.advantage)) + geom_point(aes(colour = did.win))

#
# Fit Random Forest
# #
rf <- randomForest(did.win~.,data=data, importance=TRUE, proximity=TRUE, na.action=na.roughfix, ntree=2000)
##
## Find what variables are important in predicting response
rf
importance(rf)
data$predictDidWin <- predict(rf,data[,c("seed.advantage","seed.win.loss.advantage.64","sagp.advantage")])

ggplot(data, aes(seed.advantage, sagp.advantage)) + geom_point(aes(colour = predictDidWin))
# Confusion Matrix
confusionMatrix(data=data$predictDidWin, reference=data$did.win)

#apply to current season
current <- read.table("output/current-r.csv", header=TRUE, sep=",")
current$predictWin <- predict(rf,current[,c("seed.advantage","seed.win.loss.advantage.64","sagp.advantage")])
current$probability <- predict(rf,current[,c("seed.advantage","seed.win.loss.advantage.64","sagp.advantage")], type="prob")

ggplot(current, aes(seed.advantage, sagp.advantage)) + geom_point(aes(colour = predictWin, alpha=probability[predictWin]))
write.csv(file="output/submission.csv", x=current)
