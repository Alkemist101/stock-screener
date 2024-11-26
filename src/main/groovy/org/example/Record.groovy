package org.example

import groovy.transform.Canonical

@Canonical
class Record {
    String isin
    double weight


    @Override
    public String toString() {
        return "${isin}: ${weight}"
    }
}
