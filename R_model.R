library("ggplot2")
library("caret")
library("randomForest")
library("glmnet")
library("ROCR")

data <- read.table("output/input-r.csv", header=TRUE, sep=",")

# Plot
ggplot(data, aes(seed.advantage, seed.win.loss.advantage.64)) + geom_point(aes(colour = did.win))

#
# Fit Random Forest
# #
y <-  as.factor(data[,c("did.win")])
x <- data[,c("seed.advantage","seed.win.loss.advantage.64")]
rf <- randomForest(did.win~., importance=TRUE, proximity=TRUE)
rf
data
##
## Find what variables are important in predicting response
importance(rf)
data$predictDidWin <- predict(rf,data[,c("seed.advantage","seed.win.loss.advantage.64")])

ggplot(data, aes(seed.advantage, seed.win.loss.advantage.64)) + geom_point(aes(colour = predictDidWin))
# Confusion Matrix
confusionMatrix(data=data$predictDidWin, reference=data$did.win)

#
# Fit glmnet
# #
#model <- cv.glmnet(as.matrix(data[,c("brandScore","categoryScore")]),as.factor(data[,c("didPurchase")]),nfolds=10,family="binomial")
#data$predictDidPurchase <- predict(model, as.matrix(data[,c("brandScore","categoryScore")]), type="class")
#data$predictDidPurchase <- as.factor(data$predictDidPurchase)
#ggplot(data, aes(brandScore, categoryScore)) + geom_point(aes(colour = factor(predictDidPurchase)))
# Confusion Matrix
#confusionMatrix(data=data$predictDidPurchase, reference=data$didPurchase)
