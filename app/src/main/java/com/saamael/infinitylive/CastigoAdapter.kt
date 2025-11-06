package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.saamael.infinitylive.databinding.ItemCastigoBinding

class CastigoAdapter(options: FirestoreRecyclerOptions<Castigo>) :
    FirestoreRecyclerAdapter<Castigo, CastigoAdapter.CastigoViewHolder>(options) {

    // "Línea telefónica" para hablar con la Actividad
    interface OnCastigoListener {
        fun onBorrarCastigo(id: String)
    }

    var listener: OnCastigoListener? = null

    inner class CastigoViewHolder(private val binding: ItemCastigoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(castigo: Castigo) {
            binding.tvCastigoDescripcion.text = castigo.descripcion

            binding.btnBorrarCastigo.setOnClickListener {
                // Llama a la función en la Actividad para borrar este item
                listener?.onBorrarCastigo(getItem(adapterPosition).id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CastigoViewHolder {
        val binding = ItemCastigoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CastigoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CastigoViewHolder, position: Int, model: Castigo) {
        holder.bind(model)
    }
}