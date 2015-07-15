var foundTestFolder = false;
var foundTestDocument = false;
var testFolder = companyhome.childByNamePath("/TestFolder");
if (testFolder != null) {
    var foundTestFolder = true;
    var testDoc = testFolder.childByNamePath("/TestDoc.txt")
    if (testDoc != null) {
        foundTestDocument = true;
    }
}
model["foundTestFolder"] = foundTestFolder;
model["foundTestDocument"] = foundTestDocument;