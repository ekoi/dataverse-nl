# CHANGELOG

This document lists changes to the Dataverse Network software that [DANS][DANS] has made over the years. Some of these changes were introduced by Everett in a contract agreement with Utrecht University.

Versioning does not adhere to [SemVer](http://semver.org); new releases will not have major or minor changes. DANS's changes form a patch, with sequential numbering. The first versions of the patch were not described in a changelog.

## Unreleased

Added:

- Rules-based addition of dataverse roles for users during account creation

Changed:

- A user can log in through SURFconext from any page and be redirected to the page that she left instead of a dead end
- Lots of `printStacktrace` and `println` have been replaced by calls to loggers

## Version 3.6.2-dans-12

Changed:

- Fixed a bug that caused a problem during user account lookup

## Version 3.6.2-dans-11

Changed:

- The username for users coming in via federated login is equal to their (lower-cased) email address

## Version 3.6.2-dans-10

Changed:

- The message that was shown to a user in case of an error reading user attributes from Shibboleth is more helpful

## Version 3.6.2-dans-*

The earliest DANS version of DVN 3.6.2 is based on the adapted version of DVN 3.3 that was in use at Utrecht University. Generally, the changes introduced by UU/Everett were migrated to the then current release. It is different to the original DVN in the following ways:

- UI does not allow to choose file type other than "Other", to prevent computationally expensive processing
- Support for Shibboleth was added
- Instead of the "Create account" link, there is a "Log in" link that takes the user to a Where Are You From page based on the Shibboleth configuration

[DANS]: https://www.dans.knaw.nl
