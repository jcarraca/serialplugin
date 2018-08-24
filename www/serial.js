var serial = {
    requestPermission: function(opts, successCallback, errorCallback) {
        if (typeof opts === 'function') {  //user did not pass opts
          errorCallback = successCallback;
          successCallback = opts;
          opts = {};
        }
        cordova.exec(
            successCallback,
            errorCallback,
            'Serial',
            'requestPermission',
            [{'opts': opts}]
        );
    }
};
module.exports = serial;
