/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(['require', 'backbone', 'hbs!tmpl/common/Modal'], function(require, Backbone, template) {

    var Modal = Backbone.View.extend({

        className: 'modal',

        events: {
            'click .close': function(event) {
                event.preventDefault();

                this.trigger('closeModal');

                if (this.options.content && this.options.content.trigger) {
                    this.options.content.trigger('closeModal', this, event);
                }
            },
            'click .cancel': function(event) {
                event.preventDefault();

                this.trigger('closeModal');

                if (this.options.content && this.options.content.trigger) {
                    this.options.content.trigger('closeModal', this, event);
                }
            },
            'click .ok': function(event) {
                event.preventDefault();

                this.trigger('ok');

                if (this.options.content && this.options.content.trigger) {
                    this.options.content.trigger('ok', this, event);
                }

                if (this.options.okCloses) {
                    this.close();
                }
            }
        },

        /**
         * Creates an instance of a Bootstrap Modal
         *
         * @see http://twitter.github.com/bootstrap/javascript.html#modals
         *
         * @param {Object} options
         * @param {String|View} [options.content] Modal content. Default: none
         * @param {String} [options.title]        Title. Default: none
         * @param {String} [options.okText]       Text for the OK button. Default: 'OK'
         * @param {String} [options.cancelText]   Text for the cancel button. Default: 'Cancel'. If passed a falsey value, the button will be removed
         * @param {Boolean} [options.allowCancel  Whether the modal can be closed, other than by pressing OK. Default: true
         * @param {Boolean} [options.escape]      Whether the 'esc' key can dismiss the modal. Default: true, but false if options.cancellable is true
         * @param {Boolean} [options.animate]     Whether to animate in/out. Default: false
         * @param {Function} [options.template]   Compiled underscore template to override the default one
         */
        initialize: function(options) {
            this.options = _.extend({
                title: null,
                okText: 'OK',
                focusOk: true,
                okCloses: true,
                cancelText: 'Cancel',
                allowCancel: false,
                allowBackdrop: true,
                showFooter: true,
                escape: true,
                animate: true,
                contentWithFooter: false,
                template: template,
                width: null,
                buttons: null
            }, options);
        },

        /**
         * Creates the DOM element
         *
         * @api private
         */
        render: function() {
            var $el = this.$el,
                options = this.options,
                content = options.content;

            //Create the modal container
            $el.html(options.template(options));

            // var $content = this.$content = $el.find('.modal-body');

            //Insert the main content if it's a view
            if (content && content.$el) {
                content.render();
                if (options.contentWithFooter) {
                    $el.find('.modal-content').append(content.$el);
                } else {
                    $el.find('.modal-body').html(content.$el);
                }
            } else {
                if (options.htmlContent) {
                    $el.find('.modal-body').append(options.htmlContent);
                }

            }

            // if (options.mainClass) $el.addClass(options.mainClass);

            if (options.animate) $el.addClass('fade');

            this.isRendered = true;

            return this;
        },
        onClose: function() {
            alert('close');
        },
        /**
         * Renders and shows the modal
         *
         * @param {Function} [cb]     Optional callback that runs only when OK is pressed.
         */
        open: function(cb) {
            if (!this.isRendered) this.render();
            $(".tooltip").tooltip("hide");

            var self = this,
                $el = this.$el;

            //Create it
            $el.modal(_.extend({
                keyboard: this.options.allowCancel,
                backdrop: this.options.allowBackdrop ? 'static' : true
            }, this.options.modalOptions));

            //Focus OK button
            $el.one('shown', function() {
                if (self.options.focusOk) {
                    $el.find('.btn.ok').focus();
                }

                if (self.options.content && self.options.content.trigger) {
                    self.options.content.trigger('shown', self);
                }

                self.trigger('shown');
            });

            //Adjust the modal and backdrop z-index; for dealing with multiple modals
            var numModals = Modal.count,
                $backdrop = $('.modal-backdrop:eq(' + numModals + ')'),
                backdropIndex = parseInt($backdrop.css('z-index'), 10),
                elIndex = parseInt($backdrop.css('z-index'), 10);

            $backdrop.css('z-index', backdropIndex + numModals);
            this.$el.css('z-index', elIndex + numModals);

            if (this.options.allowCancel) {
                $backdrop.one('click', function() {
                    if (self.options.content && self.options.content.trigger) {
                        self.options.content.trigger('closeModal', self);
                    }

                    self.trigger('closeModal');
                });

                $(document).one('keyup.dismiss.modal', function(e) {
                    e.which == 27 && self.trigger('closeModal');

                    if (self.options.content && self.options.content.trigger) {
                        e.which == 27 && self.options.content.trigger('shown', self);
                    }
                });
            }

            this.on('cancel', function() {
                self.close();
            });

            Modal.count++;

            //Run callback on OK if provided
            if (cb) {
                self.on('ok', cb);
            }
            $el.one('shown.bs.modal', function() {
                self.trigger('shownModal');
            });
            $el.find('.header-button').on('click', 'button', function() {
                var headerButtons = self.options.headerButtons,
                    clickedButtonIndex = $(this).data("index"),
                    clickedButtonObj = headerButtons && headerButtons[clickedButtonIndex];
                if (clickedButtonObj && clickedButtonObj.onClick) {
                    clickedButtonObj.onClick.apply(this, arguments);
                }
            });
            return this;
        },

        /**
         * Closes the modal
         */
        close: function() {
            var self = this,
                $el = this.$el;

            //Check if the modal should stay open
            if (this._preventClose) {
                this._preventClose = false;
                return;
            }
            $(".tooltip").tooltip("hide");

            $el.one('hidden.bs.modal', function onHidden(e) {
                // Ignore events propagated from interior objects, like bootstrap tooltips
                if (e.target !== e.currentTarget) {
                    return $el.one('hidden.bs.modal', onHidden);
                }
                self.remove();

                if (self.options.content && self.options.content.trigger) {
                    self.options.content.trigger('hidden.bs.modal', self);
                }

                self.trigger('hidden.bs.modal');
            });

            $el.modal('hide');

            Modal.count--;
        },

        /**
         * Stop the modal from closing.
         * Can be called from within a 'close' or 'ok' event listener.
         */
        preventClose: function() {
            this._preventClose = true;
        }
    }, {
        //STATICS

        //The number of modals on display
        count: 0
    });

    return Modal;
});