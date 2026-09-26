package com.example.ibanregistry

import android.app.Application

class IbanRegistryApplication : Application() {
    val container by lazy { AppContainer(this) }
}
