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
    lateinit var binding: FragmentCalculatorValBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_calculator_val, container, false)
        createSetting()
        binding.apply {
            ok.setOnClickListener {
                if(inputM.text.toString().isNotEmpty()) arguments?.putDouble("M",inputM.text.toString().toDouble())
                if(inputN.text.toString().isNotEmpty()) arguments?.putDouble("N",inputN.text.toString().toDouble())
                if(inputX.text.toString().isNotEmpty()) arguments?.putDouble("X",inputX.text.toString().toDouble())
                if(inputY.text.toString().isNotEmpty()) arguments?.putDouble("Y",inputY.text.toString().toDouble())
                if(inputZ.text.toString().isNotEmpty()) arguments?.putDouble("Z",inputZ.text.toString().toDouble())
                dialog?.cancel()
            }
            canc.setOnClickListener {
                dialog?.cancel()
            }
        }
        return binding.root
    }

    private fun createSetting() {
        binding.apply {
            inputM.hint = arguments?.get("M")?.toString() ?: getString(R.string.emptyString)
            inputN.hint = arguments?.get("N")?.toString() ?: getString(R.string.emptyString)
            inputX.hint = arguments?.get("X")?.toString() ?: getString(R.string.emptyString)
            inputY.hint = arguments?.get("Y")?.toString() ?: getString(R.string.emptyString)
            inputZ.hint = arguments?.get("Z")?.toString() ?: getString(R.string.emptyString)
        }
    }

    /*override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            builder.setView(R.layout.fragment_calculator_val)
                .setPositiveButton(getString(R.string.ok),DialogInterface.OnClickListener{ dialog, id ->

                })
                .setNegativeButton(getString(R.string.canc),DialogInterface.OnClickListener{ dialog, id ->

                })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }*/
}