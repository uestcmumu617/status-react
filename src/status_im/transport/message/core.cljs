(ns status-im.transport.message.core
  (:require [re-frame.core :as re-frame] [status-im.utils.handlers :as handlers]
            [status-im.data-store.messages :as data-store.messages]
            [status-im.ui.screens.contacts.events :as contacts.events]
            [status-im.chat.models :as chat.models]
            [status-im.constants :as constants]))

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

;;TODO (yenda) this is probably not the place to have these

;; Contacts

(defn receive-contact-request
  [public-key
   {:keys [name profile-image address fcm-token]}
   {{:contacts/keys [contacts] :as db} :db :as cofx}]
  (when-not (get contacts public-key)
    (let [contact-props {:whisper-identity public-key
                         :public-key       public-key
                         :address          address
                         :photo-path       profile-image
                         :name             name
                         :fcm-token        fcm-token
                         :pending?         true}
          chat-props    {:name         name
                         :chat-id      public-key
                         :contact-info (prn-str contact-props)}]
      (handlers/merge-fx cofx
                         {:db           (update-in db [:contacts/contacts public-key] merge contact-props)
                          :save-contact contact-props}
                         (chat.models/add-chat public-key chat-props)))))

(defn receive-contact-request-confirmation
  [public-key {:keys [name profile-image address fcm-token]}
   {{:contacts/keys [contacts] :as db} :db :as cofx}]
  (when-let [contact (get contacts public-key)]
    (let [contact-props {:whisper-identity public-key
                         :address          address
                         :photo-path       profile-image
                         :name             name
                         :fcm-token        fcm-token}
          chat-props    {:name    name
                         :chat-id public-key}]
      (handlers/merge-fx cofx
                         {:db           (update-in db [:contacts/contacts public-key] merge contact-props)
                          :save-contact contact-props}
                         (chat.models/upsert-chat chat-props)))))

(defn receive-contact-update [chat-id public-key {:keys [name profile-image]} {:keys [db now] :as cofx}]
  (let [{:keys [chats current-public-key]} db]
    (when (not= current-public-key public-key)
      (let [prev-last-updated (get-in db [:contacts/contacts public-key :last-updated])]
        (when (<= prev-last-updated now)
          (let [contact {:whisper-identity public-key
                         :name             name
                         :photo-path       profile-image
                         :last-updated     now}]
            (if (chats public-key)
              (handlers/merge-fx cofx
                                 (contacts.events/update-contact contact)
                                 (chat.models/update-chat {:chat-id chat-id
                                                           :name    name}))
              (contacts.events/update-contact contact cofx))))))))

;; Seen messages
(defn receive-seen
  [chat-id sender message-ids {:keys [db]}]
  (when-let [seen-messages-ids (-> (get-in db [:chats chat-id :messages])
                                   (select-keys message-ids)
                                   keys)]
    {:db (reduce (fn [new-db message-id]
                   (assoc-in new-db [:chats chat-id :messages message-id :user-statuses sender] :seen))
                 db
                 seen-messages-ids)}))

;; Group chat
(defn participants-added [chat-id added-participants-set {:keys [db] :as cofx}]
  (when (seq added-participants-set)
    {:db (update-in db [:chats chat-id :contacts] concat (mapv #(hash-map :identity %) added-participants-set))
     :data-store/add-chat-contacts [chat-id added-participants-set]}))

(defn participants-removed [chat-id removed-participants-set {:keys [now db] :as cofx}]
  (when (seq removed-participants-set)
    (let [{:keys [is-active timestamp]} (get-in db [:chats chat-id])]
      ;;TODO: not sure what this condition is for
      (when (and is-active (>= now timestamp))
        {:db (update-in db [:chats chat-id :contacts]
                        (partial remove (comp removed-participants-set :identity)))
         :data-store/remove-chat-contacts [chat-id removed-participants-set]}))))
