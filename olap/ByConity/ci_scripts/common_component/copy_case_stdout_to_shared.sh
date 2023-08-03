# copy .stdout after ci finished, create fodler structure like   */shared/case_stdout/<suite name>/<case name>.stdout
ARTIFACT_FOLDER_PATH="/test_output/case_stdout"
QURIES_FODLER="/${GITHUB_WORKSPACE}/tests/queries"

for FILE_PATH in $(find ${QURIES_FODLER} -name *.stdout)  # kubectl  get pods -n cnch | awk '{print $1}' | grep server
  do
    echo ">> find .stdout $FILE_PATH"
    SUITE_FODLER_NAME=$(cut -d'/' -f6 <<<$FILE_PATH)
    FILE_NAME=$(cut -d'/' -f7 <<<$FILE_PATH)
    mkdir -p "${ARTIFACT_FOLDER_PATH}/${SUITE_FODLER_NAME}"
    cp ${FILE_PATH} ${ARTIFACT_FOLDER_PATH}/${SUITE_FODLER_NAME}/${FILE_NAME}
done
