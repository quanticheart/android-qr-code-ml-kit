/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quanticheart.qrcodezxing.custonView.bottomSheetResult.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quanticheart.qrcodezxing.R
import kotlinx.android.synthetic.main.barcode_field.view.*

/** Presents a list of field info in the detected barcode.  */
internal class BarcodeFieldAdapter(
    recyclerView: RecyclerView,
    private val barcodeFieldList: List<BarcodeField>
) : RecyclerView.Adapter<BarcodeFieldAdapter.BarcodeFieldViewHolder>() {

    init {
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = this@BarcodeFieldAdapter
        }
    }

    private fun createView(parent: ViewGroup): BarcodeFieldViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.barcode_field, parent, false)
        return BarcodeFieldViewHolder(view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeFieldViewHolder =
        createView(parent)

    override fun onBindViewHolder(holder: BarcodeFieldViewHolder, position: Int) =
        holder.bindBarcodeField(barcodeFieldList[position])

    override fun getItemCount(): Int = barcodeFieldList.size

    inner class BarcodeFieldViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindBarcodeField(barcodeField: BarcodeField) {
            itemView.barcode_field_label.text = barcodeField.label
            itemView.barcode_field_value.text = barcodeField.value
        }
    }
}
