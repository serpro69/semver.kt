SHELL  := env $(SHELL)
.DEFAULT_GOAL := help

# https://stackoverflow.com/a/10858332
# Check that given variables are set and all have non-empty values,
# die with an error otherwise.
#
# Params:
#   1. Variable name(s) to test.
#   2. (optional) Error message to print.
check_defined = \
    $(strip $(foreach 1,$1, \
        $(call __check_defined,$1,$(strip $(value 2)))))
__check_defined = \
    $(if $(value $1),, \
      $(error Undefined $1$(if $2, ($2))))

__java_version_ok := $(shell java -version 2>&1|grep '17.0' >/dev/null; printf $$?)

.PHONY: _check_java
_check_java: ## Checks current java version (mostly used in other targets)
ifneq ($(__java_version_ok),$(shell echo 0))
	$(error "Expected java 17.0.x")
endif

.PHONY: test
test: ## Runs tests for the project
	./gradlew test functionalTest

.PHONY: local
local: _check_java ## Publishes artifacts to local repos
	# publish to local maven
	./gradlew clean publishToMavenLocal publishAllPublicationsToLocalPluginRepoRepository -Pversion='0.0.0-dev'

.PHONY: release
release: _check_java test ## Publishes the next release
	# publish to sonatype and gradle-plugin-portal and close staging repo
	./gradlew publishToSonatype closeSonatypeStagingRepository publishPlugins tag -Prelease --info
	# push git tag
	git push origin --tags

.PHONY: help
help: ## Displays this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
