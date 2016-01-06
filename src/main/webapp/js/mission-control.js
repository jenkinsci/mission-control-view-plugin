function format_date(dt) {
  return dt.getFullYear()
    + '-' + (dt.getMonth()<9?'0':'') + (dt.getMonth() + 1)
    + '-' + (dt.getDate()<10?'0':'') + dt.getDate()
    + ' ' + (dt.getHours()<10?'0':'') + dt.getHours()
    + ':' + (dt.getMinutes()<10?'0':'') + dt.getMinutes()
    + ':' + (dt.getSeconds()<10?'0':'') + dt.getSeconds();
}

function format_interval(iv) {
  if (iv < 1000) { return iv + 'ms'; }

  ivStr = '';
  // Days
  if (iv > 86400000) {
    ivStr = Math.floor(iv/86400000) + 'd';
    iv = iv - Math.floor(iv/86400000)*86400000;
  }
  // Hours
  if (iv > 3600000) {
    ivStr += ' ' + Math.floor(iv/3600000) + 'h';
    iv = iv - Math.floor(iv/3600000)*3600000;
  }
  // Minutes
  if (iv > 60000) {
    ivStr += ' ' + Math.floor(iv/60000) + 'm';
    iv = iv - Math.floor(iv/60000)*60000;
  }
  // Seconds
  if (iv > 1000)
    ivStr += ' ' + Math.floor(iv/1000) + 's';
  return ivStr;
}

function reload_jenkins_build_queue(tableSelector, jenkinsUrl) {
  $.getJSON( jenkinsUrl + '/queue/api/json', function( data ) {
    // Remove all existing rows
    $(tableSelector + ' tbody').find('tr').remove(); 
    i = 0;
    $.each( data.items, function( key, val ) {
      i++;
      if (i > 13) {
        return;
      }
      startDate = new Date(val.inQueueSince);
      now = new Date();
      waitingFor = now.getTime() - val.inQueueSince;
      taskName = val.task.name.replace(/(,?)\w*=/g, "$1");
      newRow = '<tr><td class="text-left">' + taskName + '</td><td>' + format_date(startDate) + '</td><td>' + format_interval(waitingFor) + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_node_statuses(tableSelector, jenkinsUrl, nodeStatuses) {
  $.getJSON( jenkinsUrl + '/computer/api/json', function( data ) {
    // Remove all existing rows
    $(tableSelector + ' tbody').find('tr').remove(); 
    $.each( data.computer, function( key, val ) {
      statusText = !val.offline ? nodeStatuses['Online'] : nodeStatuses['Offline'];
      status = !val.offline ? '' : 'danger';
      newRow = '<tr class="' + status + '"><td class="text-left">' + val.displayName + '</td><td>' + statusText + '</td><td>' + val.numExecutors + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_build_history(tableSelector, viewUrl) {
  $.getJSON( viewUrl + '/api/json', function( data ) {
    // Remove all existing rows
    $(tableSelector + ' tbody').find('tr').remove();
    i = 0;
    $.each( data.builds, function( key, val ) {
      i++;
      if (i > 13) {
        return;
      }
      dt = new Date(val.startTime + val.duration);
      jobName = val.buildName.replace(/(.*) #.*/, '$1');
      switch (val.result) {
        case 'SUCCESS':
          classes = '';
          break;
        case 'FAILURE':
          classes = 'danger';
          break;
        case 'ABORTED':
        case 'UNSTABLE':
          classes = 'warning';
          break;
        case 'BUILDING':
          classes = 'info invert-text-color';
          break;
        default:
          console.log('Job: ' + val.jobName + ' Result: ' + val.result);
          classes = '';
      }
      newRow = '<tr class="' + classes + '"><td class="text-left">' + jobName + '</td><td>' + val.number + '</td><td>' + format_date(dt) + '</td><td>' + format_interval(val.duration) + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_job_statuses(divSelector, viewUrl, buttonClass) {
  $.getJSON( viewUrl + '/api/json', function( data ) {
    // Remove all existing divs
    $(divSelector + ' button').remove();
    $.each( data.allJobsStatuses, function( key, val ) {
      switch (val.status) {
        case 'SUCCESS':
          classes = 'btn-success';
          break;
        case 'FAILURE':
          classes = 'btn-danger';
          break;
        case 'ABORTED':
        case 'UNSTABLE':
          classes = 'btn-warning';
          break;
        case 'NOTBUILT':
          classes = 'invert-text-color';
          break;
        case 'BUILDING':
          classes = 'btn-info invert-text-color';
          break;
        default:
          console.log('Job: ' + val.jobName + ' Status: ' + val.status);
          classes = 'btn-primary';
      }
      newDiv = '<button class="btn ' + buttonClass + ' ' + classes + ' col-lg-6">' + val.jobName + '</button>';
      $(divSelector).append(newDiv);
    });
  });
}
