package me.cpele.tnpr.origdest

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import me.cpele.afk.Event
import me.cpele.afk.ViewModelFactory
import me.cpele.afk.exhaust
import me.cpele.tnpr.R
import me.cpele.tnpr.origdest.OriginDestinationViewModel.Effect

class OriginDestinationFragment : Fragment() {

    companion object {
        fun newInstance() = OriginDestinationFragment()
    }

    private val viewModel: OriginDestinationViewModel by viewModels {
        ViewModelFactory {
            OriginDestinationViewModel(
                activity?.application
                    ?: throw IllegalStateException("Parent Activity of ${this::class.qualifiedName} should be attached")
            )
        }
    }

    private lateinit var originButton: Button
    private lateinit var destinationButton: Button
    private lateinit var instructionsText: TextView
    private lateinit var actionButton: View

    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.origin_destination_fragment, container, false)

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = context as? Listener
            ?: throw IllegalStateException(
                "${context::class.qualifiedName} has to implement ${Listener::class.qualifiedName}"
            )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intention = OriginDestinationFragmentArgs
            .fromBundle(requireArguments())
            .let { args ->
                OriginDestinationViewModel.Action.Load(
                    args.originId,
                    args.originLabel,
                    args.destinationId,
                    args.destinationLabel
                )
            }

        viewModel.dispatch(intention)

        originButton = view.findViewById(R.id.od_origin_button)
        destinationButton = view.findViewById(R.id.od_destination_button)
        instructionsText = view.findViewById(R.id.od_instructions_text)
        actionButton = view.findViewById(R.id.od_action_button)

        viewModel.stateLive.observe(viewLifecycleOwner) { state -> renderState(state) }
        viewModel.eventLive.observe(viewLifecycleOwner) { event -> renderEvent(event) }

        originButton.setOnClickListener {
            viewModel.dispatch(OriginDestinationViewModel.Action.OriginClicked)
        }

        destinationButton.setOnClickListener {
            viewModel.dispatch(OriginDestinationViewModel.Action.DestinationClicked)
        }

        actionButton.setOnClickListener {
            viewModel.dispatch(OriginDestinationViewModel.Action.ActionClicked)
        }
    }

    private fun renderState(state: OriginDestinationViewModel.State?) {
        if (state == null) {
            throw IllegalStateException("State should not be null")
        }
        originButton.text = state.originLabel
        destinationButton.text = state.destinationLabel
        instructionsText.text = state.instructions
        actionButton.isEnabled = state.isActionAllowed
    }

    private fun renderEvent(event: Event<Effect>) {
        event.consume { effect ->
            when (effect) {
                is Effect.NavigateToAutosuggest.Origin ->
                    listener?.openAutosuggestOrigin(this, effect.id, effect.label)
                is Effect.NavigateToAutosuggest.Destination ->
                    listener?.openAutosuggestDestination(this, effect.id, effect.label)
                is Effect.NavigateToTripSelection ->
                    listener?.openTripSelection(
                        this,
                        effect.originId,
                        effect.originLabel,
                        effect.destinationId,
                        effect.destinationLabel
                    )
            }?.exhaust()
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    interface Listener {

        fun openAutosuggestOrigin(fragment: Fragment, id: String?, label: String?)

        fun openAutosuggestDestination(fragment: Fragment, id: String?, label: String?)

        fun openTripSelection(
            fragment: Fragment,
            originId: String,
            originLabel: String,
            destinationId: String,
            destinationLabel: String
        )
    }
}