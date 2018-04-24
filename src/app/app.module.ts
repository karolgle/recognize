import {BrowserModule} from '@angular/platform-browser';
import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from '@angular/core';
import {HttpClientModule} from "@angular/common/http";

import {AppComponent} from './app.component';

import {UploadFileService} from "./upload-file.service";
import {MatButtonModule, MatCardModule, MatGridListModule, MatListModule, MatProgressSpinnerModule} from "@angular/material";
import {MatFileUploadModule} from "angular-material-fileupload";

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    MatButtonModule,
    MatCardModule,
    MatFileUploadModule,
    MatGridListModule,
    MatProgressSpinnerModule,
    MatListModule
  ],
  schemas: [ CUSTOM_ELEMENTS_SCHEMA ],
  providers: [UploadFileService],
  bootstrap: [AppComponent]
})
export class AppModule {


}
