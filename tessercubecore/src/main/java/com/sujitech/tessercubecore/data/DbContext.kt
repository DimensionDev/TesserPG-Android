package com.sujitech.tessercubecore.data

import com.sujitech.tessercubecore.appContext
import io.requery.Persistable
import io.requery.android.sqlite.DatabaseSource
import io.requery.reactivex.KotlinReactiveEntityStore
import io.requery.sql.KotlinEntityDataStore


object DbContext {

    val data: KotlinReactiveEntityStore<Persistable> by lazy {
        val source = DatabaseSource(appContext, Models.DEFAULT, 2)
        KotlinReactiveEntityStore<Persistable>(KotlinEntityDataStore(source.configuration))
    }
}


