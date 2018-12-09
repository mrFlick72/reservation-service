package it.valeriovaudi.lab.reservationservice.web.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "r2dbc.datasource")
class R2dbcCongfig {
    var host: String = "localhost"
    var database: String = ""
    var username: String = ""
    var password: String = ""
    override fun toString(): String {
        return "R2dbcCongfig(host='$host', database='$database', username='$username', password='$password')"
    }


}