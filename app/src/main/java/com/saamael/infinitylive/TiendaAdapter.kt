package com.saamael.infinitylive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.saamael.infinitylive.databinding.ItemTiendaBinding

class TiendaAdapter(
    options: FirestoreRecyclerOptions<ShopItem>,
    private val onComprarClick: (ShopItem) -> Unit
) : FirestoreRecyclerAdapter<ShopItem, TiendaAdapter.TiendaViewHolder>(options) {

    inner class TiendaViewHolder(private val binding: ItemTiendaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShopItem) {
            binding.tvItemNombre.text = item.nombre
            binding.tvItemDescripcion.text = item.descripcion
            binding.btnItemPrecio.text = "\uD83D\uDCB0 ${item.precio}"

            // Lógica simple para iconos
            if (item.iconName == "custom") {
                binding.ivItemIcon.setImageResource(android.R.drawable.star_big_on) // Icono para los creados
            } else {
                binding.ivItemIcon.setImageResource(R.drawable.storeicon) // Icono por defecto
            }

            binding.btnItemPrecio.setOnClickListener {
                onComprarClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiendaViewHolder {
        val binding = ItemTiendaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TiendaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TiendaViewHolder, position: Int, model: ShopItem) {
        holder.bind(model)
    }
}