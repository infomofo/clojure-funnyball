library("ggplot2")
library("caret")
library("randomForest")
library("glmnet")
library("ROCR")

# write.table(final_data, file="~/Desktop/interview_data.csv", sep=",")
data <- read.table("output/input-r.csv", header=TRUE, sep=",")

#
# Fit Random Forest
# #
predictors <- data[,c("seed-advantage","seed-win-loss-advantage-64")]
fit <- randomForest(did.win~., data=data, importance=TRUE, proximity=TRUE)
#rf <- randomForest(x=, y=as.factor(data[,c("did-win")]), importance=TRUE, proximity=TRUE)
data$predictDidWin <- predict(fit,data[,c("seed.advantage","seed.win.loss.advantage.64")])
# plot didPurchase
ggplot(data, aes(seed.advantage, seed.win.loss.advantage.64)) + geom_point(aes(colour = factor(predictDidWin)))
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
