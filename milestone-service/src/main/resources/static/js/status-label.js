// The asterisk leads on purpose: when a narrow column cuts a label short, the end goes first, so the front survives.
function statusLabel(status, needsReview) {
    return (needsReview ? '*' : '') + status;
}
