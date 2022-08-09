/*
 * jquery.ajax-retry
 * https://github.com/johnkpaul/jquery-ajax-retry
 *
 * Copyright (c) 2012 John Paul
 * Licensed under the MIT license.
 */

(function($) {

  // enhance all ajax requests with our retry API
  $.ajaxPrefilter(function(options, originalOptions, jqXHR){
    jqXHR.retry = function(opts){
      if(opts.timeout){
        this.timeout = opts.timeout;
      }
      return this.pipe(null, pipeFailRetry(this, opts.times));
    };
  });

  // generates a fail pipe function that will retry `jqXHR` `times` more times
  function pipeFailRetry(jqXHR, times){

    // takes failure data as input, returns a new deferred
    return function(input, status, msg){
      var ajaxOptions = this;
      var output = new $.Deferred();

      // whenever we do make this request, pipe its output to our deferred
      function nextRequest() {
        $.ajax(ajaxOptions)
          .retry({times:times-1})
          .pipe(output.resolve, output.reject);
      }

      if(times > 1){
        // time to make that next request...
        if(jqXHR.timeout !== undefined){
          setTimeout(nextRequest, jqXHR.timeout);
        } else {
          nextRequest();
        }
      } else {
        // no times left, reject our deferred with the current arguments
        output.rejectWith(this, arguments);
      }

      return output;
    };
  }

}(jQuery));
