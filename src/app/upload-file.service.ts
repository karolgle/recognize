import {Injectable} from '@angular/core';
import {HttpClient, HttpEvent, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';

@Injectable()
export class UploadFileService {

  constructor(private http: HttpClient) {
  }

  pushFileToServer(fileSelfie: File, fileIdDocFront: File, fileIdDocBack: File): Observable<HttpEvent<{}>> {
    const formdata: FormData = new FormData();

    formdata.append('selfie', fileSelfie);
    formdata.append('id_front', fileIdDocFront);
    formdata.append('id_back', fileIdDocBack);

    const req = new HttpRequest('POST', '/api/collect', formdata, {
      reportProgress: true
    });

    return this.http.request(req);
  }

  getFiles(): Observable<any> {
    return this.http.get('/api/getallfiles');
  }
}
