@file:OptIn(ExperimentalMaterial3Api::class)

package com.awaytime.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.awaytime.app.ui.navigation.AwayTimeNavigation
import com.awaytime.app.ui.theme.AwayTimeTheme

@Composable
fun AwayTimeApp() {
    AwayTimeNavigation()
}

@Preview(showBackground = true)
@Composable
fun AwayTimeAppPreview() {
    AwayTimeTheme {
        AwayTimeApp()
    }
}