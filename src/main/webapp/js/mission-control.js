function format_date(dt) {
  return dt.getFullYear()
    + '-' + (dt.getMonth()<10?'0':'') + dt.getMonth()
    + '-' + (dt.getDay()<10?'0':'') + dt.getDay()
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

function reload_jenkins_node_statuses(divSelector, jenkinsUrl, nodeStatuses) {
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
      switch (true) {
        case /stable.*/.test(val.status):
        case /.*back.*/.test(val.status):
          status = '';
          break;
        case /.*broken.*/.test(val.status):
          status = 'danger';
          break;
        case /.*aborted.*/.test(val.status):
        case /.*test.*fail.*/.test(val.status):
          status = 'warning';
          break;
        case val.status == '?':
          status = 'info';
          break;
        default:
          console.log('Job: ' + jobName + ' Status: ' + val.status);
          status = 'warning';
      }
      newRow = '<tr class="' + status + '"><td class="text-left">' + jobName + '</td><td>' + val.number + '</td><td>' + format_date(dt) + '</td><td>' + format_interval(val.duration) + '</td></tr>';
      $(tableSelector + ' tbody').append(newRow);
    });
  });
}

function reload_jenkins_job_statuses(divSelector, jenkinsUrl, buttonClass) {
  $.getJSON( jenkinsUrl + '/api/json', function( data ) {
    // Remove all existing divs
    $(divSelector + ' button').remove();
    $.each( data.jobs, function( key, val ) {
      switch (val.color) {
        case 'blue':
          status = 'btn-success';
          break;
        case 'red':
          status = 'btn-danger';
          break;
        case 'aborted':
        case 'yellow':
        case 'notbuilt':
          status = 'btn-warning';
          break;
        case 'aborted_anime':
        case 'blue_anime':
        case 'red_anime':
          status = 'btn-info';
          break;
        default:
          console.log('Job: ' + val.name + ' Color: ' + val.color);
          status = 'btn-primary';
      }
      newDiv = '<button class="btn ' + buttonClass + ' ' + status + ' col-lg-6">' + val.name + '</button>';
      $(divSelector).append(newDiv);
    });
  });
}
