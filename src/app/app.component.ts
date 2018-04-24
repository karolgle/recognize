import {Component} from '@angular/core';
import {UploadFileService} from "./upload-file.service";
import {HttpEventType, HttpResponse} from "@angular/common/http";
import {ImageComparisonWebInfo} from "./image-comparison-web-info.model";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  selectedFiles: FileList;
  progress: { percentage: number } = {percentage: 0};
  urlSelfie: any;
  urlIdDocFront: any;
  urlIdDocBack: any;

  fileSelfie: File;
  fileIdDocFront: File;
  fileIdDocBack: File;

  imageComparisonWebInfo: ImageComparisonWebInfo;
  showProgressIcon: boolean = false;


  constructor(private uploadService: UploadFileService) {
  }

  ngOnInit() {
    this.urlSelfie = this.urlIdDocFront = this.urlIdDocBack = "assets\\images\\empty_profile.png"
  }

  selectFile(event) {
    const file = event.target.files.item(0);
    const controlId = event.target.id;
    if (!file.type.match('image.*')) {
      alert('invalid format!');
    }

    var reader = new FileReader();

    reader.onload = (event: any) => {
      switch (controlId) {
        case "selfie":
          this.urlSelfie = event.target.result;
          this.fileSelfie = file;
          break;
        case "idDocFront":
          this.urlIdDocFront = event.target.result;
          this.fileIdDocFront = file;
          break;
        case "idDocBack":
          this.urlIdDocBack = event.target.result;
          this.fileIdDocBack = file;
          break;
      }
    }

    reader.readAsDataURL(file);
  }

  upload() {
    this.progress.percentage = 0;
    this.showProgressIcon = true;
    this.uploadService.pushFileToServer(this.fileSelfie, this.fileIdDocFront, this.fileIdDocBack).subscribe(event => {
      if (event.type === HttpEventType.UploadProgress) {
        this.progress.percentage = Math.round(100 * event.loaded / event.total);
      } else if (event instanceof HttpResponse) {
        console.log('File is completely uploaded and processed!');
        console.log(event);
        //console.log(this.letSee.find(item => item === "faceDetected"))
        this.imageComparisonWebInfo = Object.assign(new ImageComparisonWebInfo(), event.body);
        this.imageComparisonWebInfo.mrz = this.imageComparisonWebInfo.mrz.replace('MrzData(', '')
                                                                         .replace(')', '')
                                                                         .replace(', ]', ']');

        this.showProgressIcon = false;
      }
    }, error1 => {
      console.log("error");
      this.showProgressIcon = false;
    }, () => {
      console.log("completed");
      this.showProgressIcon = false;
    });

    this.selectedFiles = undefined;
  }

  uploadAllDisabled() {
    return !this.fileSelfie || !this.fileIdDocFront || !this.fileIdDocBack
  }
}

class PropertyItem {
  key: string;
  value: any;

  constructor(key: string, value: any) {
    this.key = key;
    this.value = value;
  }
}
