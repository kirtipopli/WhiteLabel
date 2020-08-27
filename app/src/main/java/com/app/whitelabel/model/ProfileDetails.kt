package com.app.whitelabel.model

import java.io.Serializable

data class ProfileDetails(
    var firstName: String = "",
    var middleName: String = "",
    var lastName: String = "",
    var name: String = ""
) : Serializable {
}