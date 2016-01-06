function StatusBar(element) {

    this.userBudget = function () {
        return element.find(".user-budget .sbar__pill");
    };
    this.postRoundBudgetRemaining = function () {
        return element.find(".postroundbudget-remaining .sbar__pill");
    }

};
