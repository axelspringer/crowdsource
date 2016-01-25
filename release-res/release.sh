#!/bin/bash
set -ev
BRANCH_TO_RELEASE="master"
BRANCH_TRIGGERING_RELEASE="release-trigger"

if [ "${TRAVIS_BRANCH}" = ${BRANCH_TRIGGERING_RELEASE} ]; then
    echo "Going to deploy release ${BRANCH_TO_RELEASE} ..."
    git clone --depth=50 --branch=${BRANCH_TO_RELEASE} git://github.com/as-ideas/crowdsource.git release_src
    cd release_src
    pwd | xargs echo "Operating in path: "
    openssl aes-256-cbc -d -a -pass pass:${GPG_ENCR_KEY} -in release-res/secring.gpg.encr -out release-res/local.secring.gpg
    gpg --import release-res/pubring.gpg
    gpg --allow-secret-key-import --import release-res/local.secring.gpg
    gpg --list-keys
    echo "Branch is ${TRAVIS_BRANCH}"
    git config --global user.email "thomas.wendzinski@asideas.de"
    git config --global user.name "Travis CI Release"
    git config credential.helper "store --file=.git/credentials"
    echo "https://${TRAVIS_GITHUB_TK}:@github.com" > .git/credentials
    cat .git/HEAD|xargs echo "Head is: "

    PASSPHRASE_STRING="-Darguments=-Dgpg.passphrase="${GPG_PASSPHRASE}
    mvn -B release:clean release:prepare --settings release-res/settings.xml -DskipTests
    mvn -B -Prelease -Prelease-sign-artifacts release:perform ${PASSPHRASE_STRING} --settings release-res/settings.xml -DskipTests -Dgit.branch=${TRAVIS_BRANCH}
else
    echo "Going to test and deploy snapshot release ${TRAVIS_BRANCH} ..."
    mvn deploy -Prelease --settings release-res/settings.xml -Dgit.branch=${TRAVIS_BRANCH}
fi
exit $?
