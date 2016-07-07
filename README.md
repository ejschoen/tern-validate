# tern-validate

A Leiningen plugin to validate that a database migrated by tern is at the expected schema version level for the library that uses it.

## Usage

1. Put `[cc.artifice/tern-validate "0.1.0"]` into the `:plugins` vector of the project.clj file for the project that uses tern.
2. Install your project (`lein install`) or run under the repl (`lein repl`).
3. Initialize a korma connection to your database.
4. Call `tern-validate.core/validate`, which will return `true` or `false`.  You may pass an optional callback function, which
must accept a single argument, a map, that describes the validation status.

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

At present, the schema version table must be `schema_versions` and the migrations directory must be `migrations/`, directly under the project directory.  Your project must use and initialize [korma](http://sqlkorma.com/).

## License

Copyright © 2016 i2k Connect LLC

Distributed under the Eclipse Public License version 1.0.
