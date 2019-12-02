/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example.android.marsrealestate.overview

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.marsrealestate.R
import com.example.android.marsrealestate.databinding.GridViewItemBinding
import com.example.android.marsrealestate.network.MarsProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ITEM_VIEW_TYPE_HEADER = 0
private val ITEM_VIEW_TYPE_ITEM = 1

/**
 * This class implements a [RecyclerView] [ListAdapter] which uses Data Binding to present [List]
 * data, including computing diffs between lists.
 * @param onClick a lambda that takes the
 */
class PhotoGridAdapter( val onClickListener: OnClickListener , val app: Application) :
        ListAdapter<DataItem, RecyclerView.ViewHolder>(DiffCallback) {
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    fun addHeaderAndSubmitList(list: List<MarsProperty>?) {
        adapterScope.launch {
            val items = when (list) {
                null -> listOf(DataItem.Header)
                else -> listOf(DataItem.Header) + list.map { DataItem.MarsPropertyItem(it) }
            }
            withContext(Dispatchers.Main) {
                submitList(items)
            }
        }
    }
    class MarsPropertyViewHolder(private var binding: GridViewItemBinding, val app: Application):
            RecyclerView.ViewHolder(binding.root) {
        fun bind(marsProperty: MarsProperty) {

            marsProperty.displayedPrice = {
                app.applicationContext.getString(
                        when (marsProperty.isRental) {
                            true -> R.string.display_price_monthly_rental
                            false -> R.string.display_price
                        }, marsProperty.price)
            }()

            marsProperty.displayedType = {
                app.applicationContext.getString(R.string.display_type,
                        app.applicationContext.getString(
                                when(marsProperty.isRental) {
                                    true -> R.string.type_rent
                                    false -> R.string.type_sale
                                }))
            }()

            binding.property = marsProperty
            // This is important, because it forces the data binding to execute immediately,
            // which allows the RecyclerView to make the correct view size measurements
            binding.executePendingBindings()
        }
    }

    /**
     * Allows the RecyclerView to determine which items have changed when the [List] of [MarsProperty]
     * has been updated.
     */
    companion object DiffCallback : DiffUtil.ItemCallback<DataItem>() {
        override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return oldItem.id == newItem.id
        }
    }

    /**
     * Create new [RecyclerView] item views (invoked by the layout manager)
     */
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder {
        return  when(viewType) {
            ITEM_VIEW_TYPE_HEADER -> TextViewHolder.from(parent)
            ITEM_VIEW_TYPE_ITEM -> MarsPropertyViewHolder(GridViewItemBinding.inflate(LayoutInflater.from(parent.context)), app)
            else ->throw ClassCastException("Unknown viewType ${viewType}")
        }
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager)
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is MarsPropertyViewHolder -> {
                val item = getItem(position) as DataItem.MarsPropertyItem
                holder.itemView.setOnClickListener {
                    onClickListener.onClick(item.marsProperty)
                }
                holder.bind(item.marsProperty)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DataItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DataItem.MarsPropertyItem -> ITEM_VIEW_TYPE_ITEM
        }
    }
    class TextViewHolder(view: View): RecyclerView.ViewHolder(view) {
        companion object {
            fun from(parent: ViewGroup): TextViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.header,
                        parent,
                        false)
                return TextViewHolder(view)
            }
        }
    }
    /**
     * Custom listener that handles clicks on [RecyclerView] items.  Passes the [MarsProperty]
     * associated with the current item to the [onClick] function.
     * @param clickListener lambda that will be called with the current [MarsProperty]
     */
    class OnClickListener(val clickListener: (marsProperty:MarsProperty) -> Unit) {
        fun onClick(marsProperty:MarsProperty) = clickListener(marsProperty)
    }
}

sealed class DataItem {
    data class MarsPropertyItem(val marsProperty: MarsProperty): DataItem() {

        override val id:String
                get() = marsProperty.id
    }

    object Header: DataItem() {
        override val id:String
                get() = Long.MIN_VALUE.toString()
    }

    abstract val id: String
}

