package com.ironforge.cleanshots.data

/** Anything the user can move to Trash: a MediaStore row with a name and a size. */
interface Cleanable {
    val mediaId: Long
    val contentUri: String
    val displayName: String
    val size: Long
}
