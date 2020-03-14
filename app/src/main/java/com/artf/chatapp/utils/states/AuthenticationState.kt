package com.artf.chatapp.utils.states

sealed class AuthenticationState {
    class Authenticated(val userId: String) : AuthenticationState()
    object Unauthenticated : AuthenticationState()
    object InvalidAuthentication : AuthenticationState()
}