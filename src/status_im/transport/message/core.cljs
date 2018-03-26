(ns status-im.transport.message.core)

(defprotocol StatusMessage
  "Protocol for transport layed status messages"
  (send [this chat-id cofx])
  (receive [this chat-id signature cofx]))

;; TODO (yenda) implement
;; :pending
;; :discover
;; :discoveries-request
;; :discoveries-response
;; :profile
;; :online
