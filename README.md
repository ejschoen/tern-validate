# tern-validate

A Leiningen plugin to validate that a database migrated by tern is at the expected schema version level for the library that uses it.

## Usage

1. Put `[cc.artifice/tern-validate "1.0.0"]` into the `:plugins` vector of the project.clj file for the project that uses tern.
2. Install your project (`lein install`) or run under the repl (`lein repl`).
3. Call `tern-validate.core/validate2`, which will return `true` or `false`.  The function takes a map as its sole parameter, with these keys
   * runtime-version -- The database schema version as a string (e.g., "20160825231113").  Typically, this would be the largest value in the 'versions' column of the table 'schema_versions" in your database.  For example, if you are using Korma and have initialized the connection to the database, a simple query like this works: ```(:version (first (select :schema_versions (fields :version) (order :version :DESC) (limit 1))))```
    * schema-project-name -- (optional) The name of a project associated with the database, and which typically is coupled
  to the schema version.
    * project -- (optional) A leiningen project map, which might contain a `:tern` entry (a map) with a `:validation` entry.  If there is a `:validation` entry, it should be a map containing `:min-version` and/or `:max-version` values, as strings.
    * callback -- (optional) A function, which must accept a single argument, a map, that describes the validation status.

Out of the box, tern-validate will tell you if the running database exactly matches the version number of the latest migration in
the `migrations/` folder of the project that uses tern.  You may add a `:validation` key to the tern configuration in project,
which allows you to specify a range of versions that are acceptable to the library:

    ...
    :tern {:migration-dir "migrations"
           :version-table "schema_versions"
           :validation {:min-version "20160627122211" :max-version "20160714092135"}}
    ...

If min or max version is missing, the acceptable version range is open-ended. If both are missing, the database version must
exactly match the version of the latest migration.

## Limitations

At present, the migrations directory must be `migrations/`, directly under the project directory.  

## License

Copyright © 2016, 2017 i2k Connect LLC

Distributed under the Eclipse Public License version 1.0.
