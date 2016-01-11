var exec = require("cordova/exec");

var FlicGap = function () {
	
    // event methods
    this.onflic = null;
	this.ondblflic = null;
    this.onerror = null;

    var that = this;
    var successCallback = function(event) {
        if (event.type === "flic" && typeof that.onflic === "function") {
            that.onflic(event);
        } else if (event.type === "dblflic" && typeof that.ondblflic === "function") {
            that.ondblflic(event);
        }
    };
	
    var errorCallback = function(err) {
        if (typeof that.onerror === "function") {
            that.onerror(err);
        }
    };

    exec(successCallback, errorCallback, "FlicGap", "init", []);
};

module.exports = FlicGap;
