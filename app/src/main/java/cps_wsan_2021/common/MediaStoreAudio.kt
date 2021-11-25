/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cps_wsan_2021.common

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

/**
 * Simple data class to hold information about an image included in the device's MediaStore.
 */
data class MediaStoreAudio(
    val id: Long,
    val displayName: String,
    val dateAdded: String,
    val contentUri: Uri,
    val duration: Int
) {
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<MediaStoreAudio>() {
            override fun areItemsTheSame(oldItem: MediaStoreAudio, newItem: MediaStoreAudio) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MediaStoreAudio, newItem: MediaStoreAudio) =
                oldItem == newItem
        }
    }
}