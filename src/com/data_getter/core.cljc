(ns com.data-getter.core
  (:gen-class)
  (:require [fierycod.holy-lambda.response :as hr]
            [fierycod.holy-lambda.agent :as agent]
            [fierycod.holy-lambda.core :as h]
            [clojure.tools.logging :as log]
            [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [environ.core :refer [env]]
            [cognitect.aws.client.api :as aws]))

(def team-query-params {:symbol "TEAM"
                        :interval "15min"
                        :exchange "NASDAQ"})
(def base-url "https://api.twelvedata.com/quote")
(def shared-client-params {:region (System/getenv "AWS_REGION")})
(def sec-man (aws/client (merge {:api :secretsmanager} shared-client-params)))

(defn query
  "Queries the API for data"
  [apikey]

  (let [response (client/get base-url {:query-params {"symbol" (:symbol team-query-params)
                                                      "interval" (:interval team-query-params)
                                                      "exchange" (:exchange team-query-params)
                                                      "apikey" apikey}})
        body (:body response)]
    (log/info (format "data query response: %s" response))
    (cheshire/parse-string body true)))

(defn get-secrets
  "Get secrets from AWS Secrets Manager"
  []
  (let [response (aws/invoke sec-man {:op :GetSecretValue
                                     :request {:SecretId (System/getenv "AWS_SECRETS_ID")}})
        secrets (:SecretString response)]
    (cheshire/parse-string secrets true)))

(defn DataGetterLambda
  "Data getter runtime, queries TwelveData API and returns the response"
  [{:keys [event ctx] :as request}]

  (log/info (format "request: %s" request))
  (let [secrets (get-secrets)
        query-result (query (:TWELVEDATA_APIKEY secrets))]
    (log/info query-result)
    (hr/json query-result)))

;; Specify the Lambda's entry point as a static main function when generating a class file
(h/entrypoint [#'DataGetterLambda])

;; Executes the body in a safe agent context for native configuration generation.
;; Useful when it's hard for agent payloads to cover all logic branches.
(agent/in-context
 (println "I will help in generation of native-configurations"))
