/**
 * Created with IntelliJ IDEA.
 * User: eprystupa
 * Date: 3/14/13
 * Time: 3:00 PM
 */

define([], function () {

    return Backbone.Collection.extend({

        _filter: "",

        url: function () {
            return "/subscriptions" + this._filter;
        },

        withService: function (service) {
            this._filter = "/services/" + service;
            return this;
        }
    });
});