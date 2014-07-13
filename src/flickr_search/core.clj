(ns flickr-search.core
  (:require [clojure.java.io :refer [writer as-file copy input-stream output-stream]]
            [clojure.data.json :refer [read-str]])
  (:gen-class))


(def api-key "21546dfa76e045ecfb320a018bfc5680")
(def flickr-base "https://api.flickr.com/services/rest?format=json&nojsoncallback=1")
(def cc-license "&license=1,2,3,4,5,6,7")
(def search-method "flickr.photos.search")
(def info-method "flickr.photos.getinfo")
(def exif-method "flickr.photos.getexif")
(def destination "downloaded-results")

(defn service-url
  "Returns a Flickr service URL"
  [method & params]
  (apply str
         flickr-base
         "&method="
         method
         "&api_key="
         api-key
         params))

(defn search
  "Calls the Flickr search API and returns a vector
  of photo maps for the supplied search text"
  [text]
  (let [url (service-url
             search-method
             cc-license
             "&text="
             text)
        result (clojure.data.json/read-str
                (slurp url)
                :key-fn keyword)]
    (-> result :photos :photo)))

(defn download-url
  "Returns a Flickr photo URL"
  [photo]
  (let [{:keys [server farm id secret title]} photo]
    (str "https://farm"
         farm
         ".staticflickr.com/"
         server
         "/"
         id
         "_"
         secret
         "_b.jpg")))

(defn download
  "Downloads a photo into the specified folder.
  The folder is created if it does not exist.
  Returns the photo map that was passed into the function"
  [folder photo]
  (let [url (download-url photo)
        id (photo :id)
        filename (str folder "/" id ".jpg")]
    (.mkdirs (as-file folder))
    (try
      (with-open [in (input-stream url)
                  out (output-stream filename)]
        (copy in out))
      photo)))

(defn make-request
  "Return a function for requesting additional photo information"
  [method]
  (fn [id]
    (let [url (service-url method "&photo_id=" id)
          result (clojure.data.json/read-str (slurp url) :key-fn keyword)]
      (-> result :photo))))

(def request-info (make-request info-method))

(def request-exif (make-request exif-method))

(defn info
  "Return a map of additional photo information"
  [photo]
  (let [{:keys [id title]} photo
        detail (request-info id)
        exif (request-exif id)
        info {:id id
              :title title
              :datetaken (-> detail :dates :taken)
              :camera (-> exif :camera)
              :username (-> detail :owner :username)
              :realname (-> detail :owner :realname)}]
    (conj info {:description
                (-> detail :description :_content)})))

(defn format-output
  "Formats data from the info map and appends to text"
  [text info]
  (let [{:keys [id title datetaken username realname camera description]} info]
    (str text
         id
         "\n-----------"
         "\nTitle: " title
         "\nDate taken: " datetaken
         "\nUser name: " username
         "\nReal name: " realname
         "\nCamera: " camera
         "\nDescription: " description
         "\n\n\n")))

(defn output
  "Outputs the supplied list of info maps as formatted text to the console"
  [info]
  (println (reduce format-output
                   ""
                   info)))

(defn fetch-photos
  "Download image and additional information for each photo in collection"
  [photos]
  (doall
    (pmap (comp info (partial download destination))
          photos)))

(defn plus-join
  "Joins collection x with plus symbol"
  [x]
  (clojure.string/join "+" x))

(defn run
  "Run the application, writing photos to a download folder and
  outputting additional information for each photo"
  [args]
  (-> args
      plus-join
      search
      fetch-photos
      output))

(defn -main
  [& args]
  (if args
    (time
      (run args))
    (println "Usage: cmdline SEARCH-TERM")))
