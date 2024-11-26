package org.example

import com.xlson.groovycsv.CsvParser

static void main(String[] args) {
    println "Hello world!"

    def references = ["wval", "jpgl", "avantis"]

    // Step 1. Load data
    Map<String, List<Record>> dataMap = new HashMap<>()
    references.each { name ->
        dataMap.put name, loadCsvToRecords("${name}.csv")
    }

    // Step 2. Find intersect ISIN between lists
    def intersect = dataMap
            .collect { name, data -> data.collect { it.isin } }
            .findAll { it } // Remove empty lists
            .collect { it.toSet() } // Convert to sets for efficient intersection
            .inject { result, currentSet ->
                result.intersect(currentSet)
            } ?: []

    // Step 3. Filter records based on the intersected ISINs
    def records = dataMap.collectEntries { name, records ->
        [name, records.findAll { it.isin in intersect }]
    }

    // Step 4. Flatten all records and calculate aggregate weights
    def aggregated = records.collectMany { name, items ->
        items.collect { record ->
            [isin: record.isin, name: name, weight: record.weight]
        }
    }.groupBy { it.isin }.collect { isin, items ->
        def totalWeight = items.sum { it.weight } // Aggregate weights
        def recordWeightsByName = items.collectEntries { [it.name, it.weight] }
        [isin: isin, records: recordWeightsByName, totalWeight: totalWeight]
    }

    // Step 5. Sort by aggregated weight in descending order
    def candidates = aggregated.sort { -it.totalWeight }

    // Step 6. Report
    println "Found ${candidates.size()} candidates"

    candidates.each {
        def isin = it.isin
        println "-------------------------------"
        println "${isin} -> ${it.totalWeight}"
        it.records.each { k, v -> // Unpack key and value of the map
            println "\t ${k.toUpperCase(Locale.ROOT)} -> ${v}"
        }
    }

}

// Function to load CSV file into a list of Record objects
static def loadCsvToRecords(String fileName) {
    def records = []
    GemFinderScaled.class.getClassLoader().getResourceAsStream(fileName).withReader { reader ->
        def parser = new CsvParser().parse([separator: ';'], reader)
        parser.each { line ->
            records << new Record(isin: line[0], weight: Double.parseDouble(line[1] - "%"))
        }
    }
    return records
}