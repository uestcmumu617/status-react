(ns status-im.chat.handlers
  (:require [status-im.chat.models :as models] 
            [status-im.utils.handlers :as handlers] 
            status-im.chat.events))

(handlers/register-handler-fx
  :group-chat-invite-received
  (fn [{{:keys [current-public-key] :as db} :db}
       [_ {:keys                                                    [from]
           {:keys [group-id group-name contacts keypair timestamp]} :payload}]]
    (let [contacts' (keep (fn [ident]
                            (when (not= ident current-public-key)
                              {:identity ident})) contacts)
          chat      (get-in db [:chats group-id])
          new-chat  {:chat-id     group-id
                     :name        group-name
                     :group-chat  true
                     :group-admin from
                     :contacts    contacts'
                     :added-to-at timestamp
                     :timestamp   timestamp
                     :is-active   true}]
      (when (or (nil? chat)
                (models/new-update? chat timestamp))
        {:dispatch (if chat
                     [:update-chat! new-chat]
                     [:add-chat group-id new-chat])}))))
