package it.valeriovaudi.lab.reservationservice

import java.net.URI

fun URI.extractId() = this.toString().split("/").run { this[this.size - 1] }