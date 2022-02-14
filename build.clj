(ns build
  (:refer-clojure :exclude [test compile])
  (:require [clojure.tools.build.api :as b]
            [borkdude.gh-release-artifact :as gh]
            [org.corfield.build :as bb]))

(def lib 'io.lambdaforge/wanderung)
(def version (format "0.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean
  [_]
  (b/delete {:path "target"}))


(defn jar
  [opts]
  (-> opts
      (assoc :class-dir class-dir
             :src-pom "./template/pom.xml"
             :lib lib
             :version version
             :basis basis
             :jar-file jar-file
             :src-dirs ["src"])
      bb/jar))


(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/run-tests)
      bb/clean
      bb/jar))

(defn install "Install the JAR locally." [opts]
  (-> opts
      jar
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :installer :remote
             :artifact jar-file
             :pom-file (b/pom-path {:lib lib :class-dir class-dir}))
      (bb/deploy)))

(defn release
  [_]
  (-> (gh/overwrite-asset {:org "lambdaforge"
                           :repo (name lib)
                           :tag version
                           :commit (gh/current-commit)
                           :file jar-file
                           :content-type "application/java-archive"})
      :url
      println))
