package com.skeep.pscal

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.skeep.pscal.databinding.FragmentCalculatorValBinding

class CalculatorVal: DialogFragment() {
    /*lateinit var binding: FragmentCalculatorValBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_calculator_val, container, false)
        return binding.root
    }*/

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            builder.setView(inflater.inflate(R.layout.fragment_calculator_val,null))
                /*.setPositiveButton(getString(R.string.ok),DialogInterface.OnClickListener{ dialog, id ->

                })
                .setNegativeButton(getString(R.string.canc),DialogInterface.OnClickListener{ dialog, id ->

                })*/
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}